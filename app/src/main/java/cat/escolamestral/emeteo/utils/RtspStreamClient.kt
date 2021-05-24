package cat.escolamestral.emeteo.utils

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ProgressBar
import cat.escolamestral.emeteo.decode.AudioDecodeThread
import cat.escolamestral.emeteo.decode.FrameQueue
import cat.escolamestral.emeteo.decode.VideoDecodeThread
import com.alexvas.rtsp.RtspClient
import com.alexvas.utils.NetUtils
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class RtspStreamClient(
    surfaceView: SurfaceView,
    private val url: String,
    private val username: String? = null,
    private val password: String? = null,
    private val playAudio: Boolean = true,
    private val playVideo: Boolean = true,
    private val progressBar: ProgressBar? = null
) : SurfaceHolder.Callback {

    private var videoFrameQueue: FrameQueue = FrameQueue()
    private var audioFrameQueue: FrameQueue = FrameQueue()
    private var surface = surfaceView.holder.surface
    private var surfaceWidth: Int = 1920
    private var surfaceHeight: Int = 1080
    private var rtspStopped = AtomicBoolean(true)
    private var videoDecodeThread: VideoDecodeThread? = null
    private var audioDecodeThread: AudioDecodeThread? = null
    private var videoMimeType: String = ""
    private var audioMimeType: String = ""
    private var audioSampleRate: Int = 0
    private var audioChannelCount: Int = 0
    private var audioCodecConfig: ByteArray? = null
    private var rtspThread: RtspThread? = null

    init {
        surfaceView.holder.addCallback(this)
    }

    fun start() {
        rtspThread = RtspThread()
        rtspThread?.start()
        rtspStopped.set(false)
    }

    fun stop() {
        rtspStopped.set(true)
        rtspThread?.interrupt()
    }

    fun onRtspClientStarted() {
        rtspStopped.set(false)
    }

    fun onRtspClientStopped() {
        rtspStopped.set(true)
        videoDecodeThread?.interrupt()
        videoDecodeThread = null
        audioDecodeThread?.interrupt()
        audioDecodeThread = null
    }

    fun onRtspClientConnected() {
        if (videoMimeType.isNotEmpty() && playVideo) {
            videoDecodeThread = VideoDecodeThread(
                surface!!,
                videoMimeType,
                surfaceWidth,
                surfaceHeight,
                videoFrameQueue
            )
            videoDecodeThread?.start()
        }
        if (audioMimeType.isNotEmpty() && playAudio) {
            audioDecodeThread = AudioDecodeThread(
                audioMimeType,
                audioSampleRate,
                audioChannelCount,
                audioCodecConfig,
                audioFrameQueue
            )
            audioDecodeThread?.start()
        }
    }

    inner class RtspThread : Thread() {
        override fun run() {
            Handler(Looper.getMainLooper()).post { onRtspClientStarted() }
            val listener = object : RtspClient.RtspClientListener {
                override fun onRtspDisconnected() {
                    rtspStopped.set(true)
                    Handler(Looper.getMainLooper()).post { progressBar?.visibility = View.GONE }
                }

                override fun onRtspFailed(message: String?) {
                    rtspStopped.set(true)
                    Handler(Looper.getMainLooper()).post { progressBar?.visibility = View.GONE }
                    Log.d(TAG, "onRtspFailed(message=\"$message\")")
                }

                override fun onRtspConnected(sdpInfo: RtspClient.SdpInfo) {
                    Log.d(TAG, "onRtspConnected")
                    Handler(Looper.getMainLooper()).post { progressBar?.visibility = View.GONE }
                    if (sdpInfo.videoTrack != null) {
                        videoFrameQueue.clear()
                        when (sdpInfo.videoTrack?.videoCodec) {
                            RtspClient.VIDEO_CODEC_H264 -> videoMimeType = "video/avc"
                            RtspClient.VIDEO_CODEC_H265 -> videoMimeType = "video/hevc"
                        }
                        when (sdpInfo.audioTrack?.audioCodec) {
                            RtspClient.AUDIO_CODEC_AAC -> audioMimeType = "audio/mp4a-latm"
                        }
                        val sps: ByteArray? = sdpInfo.videoTrack?.sps
                        val pps: ByteArray? = sdpInfo.videoTrack?.pps
                        // Initialize decoder
                        if (sps != null && pps != null) {
                            val data = ByteArray(sps.size + pps.size)
                            sps.copyInto(data, 0, 0, sps.size)
                            pps.copyInto(data, sps.size, 0, pps.size)
                            videoFrameQueue.push(FrameQueue.Frame(data, 0, data.size, 0))
                        }
                    }
                    if (sdpInfo.audioTrack != null) {
                        audioFrameQueue.clear()
                        when (sdpInfo.audioTrack?.audioCodec) {
                            RtspClient.AUDIO_CODEC_AAC -> audioMimeType = "audio/mp4a-latm"
                        }
                        audioSampleRate = sdpInfo.audioTrack?.sampleRateHz!!
                        audioChannelCount = sdpInfo.audioTrack?.channels!!
                        audioCodecConfig = sdpInfo.audioTrack?.config
                    }

                    onRtspClientConnected()
                }

                override fun onRtspFailedUnauthorized() {
                    Log.e(TAG, "unauthorized")
                    Handler(Looper.getMainLooper()).post { progressBar?.visibility = View.GONE }
                    rtspStopped.set(true)
                }

                override fun onRtspVideoNalUnitReceived(
                    data: ByteArray,
                    offset: Int,
                    length: Int,
                    timestamp: Long
                ) {
                    if (length > 0)
                        videoFrameQueue.push(FrameQueue.Frame(data, offset, length, timestamp))
                }

                override fun onRtspAudioSampleReceived(
                    data: ByteArray,
                    offset: Int,
                    length: Int,
                    timestamp: Long
                ) {
                    if (length > 0)
                        audioFrameQueue.push(FrameQueue.Frame(data, offset, length, timestamp))
                }

                override fun onRtspConnecting() {
                    Handler(Looper.getMainLooper()).post { progressBar?.visibility = View.VISIBLE }
                    Log.d(TAG, "onRtspConnecting()")
                }
            }
            val uri: Uri = Uri.parse(url)
            val port = if (uri.port == -1) DEFAULT_RTSP_PORT else uri.port
            try {
                Log.d(TAG, "Connecting to ${uri.host.toString()}:$port...")
                val socket: Socket =
                    NetUtils.createSocketAndConnect(uri.host.toString(), port, 5000)

                // Blocking call until stopped variable is true or connection failed
                val rtspClient = RtspClient.Builder(socket, uri.toString(), rtspStopped, listener)
                    .requestVideo(playVideo)
                    .requestAudio(playAudio)
                    .withDebug(true)
                    .withUserAgent("EMeteo RTSP")
                    .withCredentials(username, password)
                    .build()

                rtspClient.execute()

                NetUtils.closeSocket(socket)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Handler(Looper.getMainLooper()).post { onRtspClientStopped() }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surface = holder.surface
        surfaceWidth = width
        surfaceHeight = height
        if (videoDecodeThread != null) {
            videoDecodeThread?.interrupt()
            videoDecodeThread =
                VideoDecodeThread(surface!!, videoMimeType, width, height, videoFrameQueue)
            videoDecodeThread?.start()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        videoDecodeThread?.interrupt()
        videoDecodeThread = null
    }

    companion object {
        private const val DEFAULT_RTSP_PORT = 554
        private val TAG: String = RtspStreamClient::class.java.simpleName
    }
}