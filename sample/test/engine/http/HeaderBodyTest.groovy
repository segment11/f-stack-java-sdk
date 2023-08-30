package engine.http

import spock.lang.Specification

class HeaderBodyTest extends Specification {
    def "Feed"() {
        given:
        def h = new HeaderBody()
        def h2 = new HeaderBody()
        def h3 = new HeaderBody()
        def h4 = new HeaderBody()
        def h5 = new HeaderBody()
        def h6 = new HeaderBody()
        def h7 = new HeaderBody()
        def h8 = new HeaderBody()
        def h9 = new HeaderBody()
        def h10 = new HeaderBody()

        when:
        def getHeader = "GET / HTTP/1.1\r\nAccept: text/html\r\n\r\n".bytes
        h.feed(getHeader)
        then:
        h.isOk
        h.action == "GET / HTTP/1.1"
        h.httpVersion == "HTTP/1.1"
        h.requestType == "GET"
        h.url == "/"
        h.contentLength() == 0
        h.headers.size() == 1

        when:
        def postHeader = "POST / HTTP/1.1\r\nContent-Length: 4\r\n\r\n".bytes
        h2.feed(postHeader)
        then:
        !h2.isOk
        h2.action == "POST / HTTP/1.1"
        h2.httpVersion == "HTTP/1.1"
        h2.requestType == "POST"
        h2.url == "/"
        h2.contentLength() == 4
        h2.headers.size() == 1

        when:
        def fullHttpHeaderWithBody = "POST / HTTP/1.1\r\nContent-Length: 4\r\n\r\n1234".bytes
        h3.feed(fullHttpHeaderWithBody)
        then:
        h3.isOk
        h3.action == "POST / HTTP/1.1"
        h3.httpVersion == "HTTP/1.1"
        h3.requestType == "POST"
        h3.url == "/"
        h3.contentLength() == 4
        h3.headers.size() == 1
        '1234' == new String(h3.body())

        when:
        def notFullHttpHeaderWithBody = "POST / HTTP/1.1\r\nContent-".bytes
        def notOkLeft = h4.feed(notFullHttpHeaderWithBody)
        then:
        !h4.isOk
        h4.headerLength == 25
        !notOkLeft.isLastFeedOk
        notOkLeft.list.size() == 1
        notOkLeft.list[0].readIndex == 0

        when:
        def notFullHttpHeaderWithBody2 = "/ HTTP/1.1\r\nContent-Length: 4\r\n\r\n1234".bytes
        def left = new LeftBuffer()
        left.add("POST ".bytes, 0)
        h5.feed(notFullHttpHeaderWithBody2, notFullHttpHeaderWithBody2.length + left.length(), 0, left)
        then:
        h5.isOk
        h5.action == "POST / HTTP/1.1"
        h5.httpVersion == "HTTP/1.1"
        h5.requestType == "POST"
        h5.url == "/"
        h5.contentLength() == 4
        h5.headers.size() == 1
        '1234' == new String(h5.body())

        when:
        def notFullHttpHeaderWithBody3 = "/ HTTP/1.1\r\nContent-Length: 4\r\n\r\n1234".bytes
        def left2 = new LeftBuffer()
        left2.add("GET / HTTP/1.1\r\nAccept: text/html\r\n\r\n0000000".bytes, 0, 0,
                "GET / HTTP/1.1\r\nAccept: text/html\r\n\r\n".bytes.length)
        left2.add("POST ".bytes, 0)
        def left3 = h6.feed(notFullHttpHeaderWithBody3, notFullHttpHeaderWithBody3.length + left2.length(), 0, left2)
        h7.feed(new byte[0], left3.length(), 0, left3)
        then:
        h6.isOk
        h6.action == "GET / HTTP/1.1"
        h6.httpVersion == "HTTP/1.1"
        h6.requestType == "GET"
        h6.url == "/"
        h6.contentLength() == 0
        h6.headers.size() == 1

        h7.isOk
        h7.action == "POST / HTTP/1.1"
        h7.httpVersion == "HTTP/1.1"
        h7.requestType == "POST"
        h7.url == "/"
        h7.contentLength() == 4
        h7.headers.size() == 1
        '1234' == new String(h7.body())

        when:
        def multiHttpHeaderWithBody = "GET / HTTP/1.1\r\nAccept: text/html\r\n\r\nGET / HTTP/1.1\r\nAccept: text/html\r\n\r\nPOST".bytes
        def left8 = h8.feed(multiHttpHeaderWithBody, multiHttpHeaderWithBody.length, 0, null)
        left8.dump()
        def left9 = h9.feed(new byte[0], left8.length(), 0, left8)
        left9.dump()
        def left10 = h10.feed(new byte[0], left9.length(), 0, left9)
        left10.dump()
        then:
        h8.isOk
        h9.isOk
        !h10.isOk
        left10.list.size() == 1
        left10.list[0].readIndex == multiHttpHeaderWithBody.length - 4
    }
}
