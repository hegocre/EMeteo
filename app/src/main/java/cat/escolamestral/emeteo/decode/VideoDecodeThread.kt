package cat.escolamestral.emeteo.decode

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.nio.ByteBuffer

class VideoDecodeThread(
    private val surface: Surface,
    private val mimeType: String,
    private val width: Int,
    private val height: Int,
    private val videoFrameQueue: FrameQueue
) : Thread() {

    override fun run() {

        val decoder = MediaCodec.createDecoderByType(mimeType)
        val format = MediaFormat.createVideoFormat(mimeType, width, height)

        decoder.configure(format, surface, null, 0)
        decoder.start()

        val bufferInfo = MediaCodec.BufferInfo()
        while (!interrupted()) {
            val inIndex: Int = decoder.dequeueInputBuffer(10000L)
            if (inIndex >= 0) {
                // fill inputBuffers[inputBufferIndex] with valid data
                val byteBuffer: ByteBuffer? = decoder.getInputBuffer(inIndex)
                byteBuffer?.rewind()

                // Preventing BufferOverflowException
//              if (length > byteBuffer.limit()) throw DecoderFatalException("Error")

                val frame: FrameQueue.Frame?
                try {
                    frame = videoFrameQueue.pop()
                    if (frame != null) {
                        byteBuffer!!.put(frame.data, frame.offset, frame.length)
                        decoder.queueInputBuffer(
                            inIndex,
                            frame.offset,
                            frame.length,
                            frame.timestamp,
                            0
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            try {
                val outIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (outIndex >= 0) {
                    decoder.releaseOutputBuffer(outIndex, bufferInfo.size != 0)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

            // All decoded frames have been rendered, we can stop playing now
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                break
            }
        }

        try {
            decoder.stop()
            decoder.release()
        } catch (ignored: IllegalStateException) {
        }

        videoFrameQueue.clear()
    }
}