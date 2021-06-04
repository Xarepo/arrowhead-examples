java -jar target/*-jar-with-dependencies.jar \
    certificates/testcloud2/temperature1.p12 \
    certificates/testcloud2/truststore.p12 \
    127.0.0.1 8443 9003 1 &

java -jar target/*-jar-with-dependencies.jar \
    certificates/testcloud2/temperature2.p12 \
    certificates/testcloud2/truststore.p12 \
    127.0.0.1 8443 9004 2

