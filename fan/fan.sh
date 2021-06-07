java -jar \
    -Dio.netty.leakDetection.level=paranoid \
    target/*-jar-with-dependencies.jar \
    certificates/testcloud2/fan.p12 \
    certificates/testcloud2/truststore.p12 \
    9001
