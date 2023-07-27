# f-stack-java-sdk

## Description
A java/groovy sdk using f-stack by jni.

## Prerequisites
- java17
- gradle7.x+
- dpdk22.11
- f-stack

## Steps

- 0. cd workspace/f-stack-java-sdk
- 1. gradle jar && mkdir output && cp build/libs/ff_jni.jar output/
- 2. gradle prepare
- 3. cd src && java -jar javacpp-1.5.9.jar -Dplatform.linkpath=/usr/local/lib/x86_64-linux-gnu -Dplatform.link=fstack ff/Invoker.java
- 4. cd .. && gradle cleanJNISo
- 5. cd sample && gradle jar
- 6. cp ../output/*.so ./build/libs
- 7. cp ${F-STACK}/lib/libfstack.so ./build/libs
- 8. cp ${F-STACK}/config.ini ./build/libs
- 9. cd build/libs && java -Djava.library.path=. -jar sample.jar --procType=primary --procId=0 --server --http --port=8080
