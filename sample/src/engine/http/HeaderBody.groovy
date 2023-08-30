package engine.http

import groovy.transform.CompileStatic

import java.nio.ByteBuffer

@CompileStatic
class HeaderBody {
    static final int HEADER_BUFFER_LENGTH = 1024 * 4

    static final String HEADER_CONTENT_LENGTH = 'Content-Length'

    static final byte r = '\r'.bytes[0]
    static final byte n = '\n'.bytes[0]
    static final byte e = ':'.bytes[0]

    private byte[] headerBuffer
    private LeftBuffer bodyBuffer

    HeaderBody() {
        this.headerBuffer = new byte[HEADER_BUFFER_LENGTH]
    }

    int headerLength = 0
    int startIndex = 0

    String action
    String requestType
    String httpVersion
    String url
    boolean isOk = false

    String lastHeaderName
    Map<String, String> headers = [:]

    private int contentLengthCache = -1

    int contentLength() {
        if (contentLengthCache === -1) {
            def s = headers[HEADER_CONTENT_LENGTH]
            contentLengthCache = s != null ? s.toInteger().intValue() : 0
        }
        contentLengthCache
    }

    byte[] body() {
        def contentLength = contentLength()
        if (contentLength > 0) {
            return bodyBuffer.readFull()
        } else {
            return null
        }
    }

    LeftBuffer feed(byte[] data, int count = data.length, int offset = 0, LeftBuffer left = null) {
        def bf = ByteBuffer.allocate(data.length)
        bf.put(data, 0, data.length)
        bf.flip()
        return feedBuffer(bf, count, offset, left)
    }

    LeftBuffer feedBuffer(ByteBuffer data, int count = data.capacity(), int offset = 0, LeftBuffer left = null) {
        def bf = headerBuffer
        int leftLength = left != null ? left.length() : 0
        while (count > 0) {
            if (left != null) {
                if (offset < leftLength) {
                    bf[headerLength] = left.read(offset)
                } else {
                    bf[headerLength] = data.get(offset - leftLength)
                }
            } else {
                bf[headerLength] = data.get(offset)
            }

            headerLength++
            offset++
            count--

            if (headerLength >= HEADER_BUFFER_LENGTH) {
                throw new IllegalStateException('header too long')
            }

            if (bf[headerLength - 1] === n && bf[headerLength - 2] === r) {
                if (action === null) {
                    action = new String(bf, startIndex, headerLength - startIndex - 2)
                    // parse action
                    def arr = action.split(' ')
                    if (arr.length > 0) {
                        requestType = arr[0]
                    }
                    if (arr.length > 1) {
                        url = arr[1]
                    }
                    if (arr.length > 2) {
                        httpVersion = arr[2]
                    }
                    startIndex = headerLength
                } else {
                    if (bf[headerLength - 3] === n && bf[headerLength - 4] === r) {
                        if (lastHeaderName !== null) {
                            headers[lastHeaderName] = new String(bf, startIndex, headerLength - startIndex - 2)
                        }

                        return checkIfLeft(left, leftLength, data)
                    } else {
                        if (lastHeaderName !== null) {
                            headers[lastHeaderName] = new String(bf, startIndex, headerLength - startIndex - 2)
                            startIndex = headerLength
                            lastHeaderName = null
                        }
                    }
                }
            } else if (bf[headerLength - 1] === e && lastHeaderName === null) {
                lastHeaderName = new String(bf, startIndex, headerLength - startIndex - 1)
                startIndex = headerLength
            }
        }

        isOk = false
        if (left != null) {
            left.reset()
        }
        def result = left != null ? left : new LeftBuffer()
        result.isLastFeedOk = false
        if (data.capacity() > 0) {
            result.addBuffer(data, 0)
        }
        return result
    }

    private LeftBuffer checkIfLeft(LeftBuffer left, int leftLength, ByteBuffer data) {
        int count = data == null ? 0 : data.capacity()
        def contentLength = contentLength()
        def totalLength = headerLength + contentLength

        if (left != null) {
            if (totalLength <= leftLength) {
                if (contentLength > 0) {
                    bodyBuffer = left.readNextLength(contentLength)
                }
                left.removeIfAllRead()
                if (count > 0) {
                    left.addBuffer(data, 0)
                }

                isOk = true
                left.isLastFeedOk = true
                return left
            } else {
                def readCount = totalLength - leftLength
                if (readCount <= count) {
                    left.addBuffer(data, readCount - contentLength)
                    if (contentLength > 0) {
                        bodyBuffer = left.readNextLength(contentLength)
                    }
                    left.removeIfAllRead()

                    isOk = true
                    left.isLastFeedOk = true
                    return left
                } else {
                    if (count > 0) {
                        left.addBuffer(data, 0)
                    }
                    isOk = false
                    left.isLastFeedOk = false
                    return left
                }
            }
        } else {
            if (totalLength <= count) {
                def leftNew = new LeftBuffer()
                leftNew.addBuffer(data, count, headerLength)

                if (contentLength > 0) {
                    bodyBuffer = leftNew.readNextLength(contentLength)
                }
                leftNew.removeIfAllRead()

                isOk = true
                leftNew.isLastFeedOk = true
                return leftNew
            } else {
                def leftNew = new LeftBuffer()
                leftNew.addBuffer(data, 0)
                isOk = false
                leftNew.isLastFeedOk = false
                return leftNew
            }
        }
    }
}
