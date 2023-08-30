package engine.http

import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j

import java.nio.ByteBuffer

@CompileStatic
@Slf4j
class LeftBuffer {

    LinkedList<One> list = []

    boolean isLastFeedOk = false

    // for debug
    void dump() {
        log.info 'is last feed ok: {}, length: {}, list size: {}', isLastFeedOk, length(), list.size()
        for (one in list) {
            log.info 'one - buffer size: {}, offset: {}, read index: {}, length: {}',
                    one.buffer.capacity(), one.offset, one.readIndex, one.length
        }
    }

    void add(byte[] bytes, int offset, int readIndex = offset, int length = bytes.length) {
        addBuffer(ByteBuffer.wrap(bytes), offset, readIndex, length)
    }

    void addBuffer(ByteBuffer buffer, int offset, int readIndex = offset, int length = buffer.capacity()) {
        list.add(new One(buffer, offset, readIndex, length))
    }

    void removeIfAllRead() {
        def it = list.iterator()
        while (it.hasNext()) {
            def one = it.next()
            if (one.readIndex >= one.length) {
                it.remove()
            } else {
                one.offset = one.readIndex
                continue
            }
        }
    }

    void reset() {
        for (one in list) {
            one.readIndex = one.offset
        }
    }

    byte[] readFull() {
        def bytes = new byte[length()]
        int destPos = 0
        for (one in list) {
            def len = one.length - one.offset
            if (len == 0) {
                continue
            }

            one.buffer.position(one.offset)
            one.buffer.get(bytes, destPos, len)
            destPos += len
        }
        bytes
    }

    LeftBuffer readNextLength(int contentLength) {
        def result = new LeftBuffer()
        int sum = 0
        for (one in list) {
            // skip all read
            if (one.readIndex >= one.length) {
                continue
            }

            def leftLength = one.length - one.readIndex
            sum += leftLength
            result.addBuffer(one.buffer, one.readIndex)

            if (sum >= contentLength) {
                one.readIndex += leftLength - (sum - contentLength)
                break
            } else {
                one.readIndex = one.length
            }
        }
        result
    }

    @CompileStatic
    @TupleConstructor
    static class One {
        ByteBuffer buffer
        int offset
        int readIndex
        int length
    }

    int length() {
        if (list.isEmpty()) {
            return 0
        }

        int len = 0
        for (one in list) {
            len += one.length - one.offset
        }
        len
    }

    byte read(int index) {
        for (one in list) {
            int len = one.length - one.offset
            if (index < len) {
                def readIndex = one.offset + index
                def b = one.buffer.get(readIndex)
                one.readIndex = readIndex + 1
                return b
            }
            index -= len
        }
        throw new IndexOutOfBoundsException()
    }

}
