package cat.escolamestral.emeteo.decode

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

class FrameQueue {

    class Frame(val data: ByteArray, val offset: Int, val length: Int, val timestamp: Long)

    val queue: BlockingQueue<Frame> = ArrayBlockingQueue(60)

    @Throws(InterruptedException::class)
    fun push(frame: Frame): Boolean {
        if (queue.offer(frame, 5, TimeUnit.MILLISECONDS)) {
            return true
        }
        return false
    }

    @Throws(InterruptedException::class)
    fun pop(): Frame? {
        try {
            return queue.poll(1000, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return null
    }

    fun clear() {
        queue.clear()
    }
}