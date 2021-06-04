java -jar target/*-jar-with-dependencies.jar \
    certificates/testcloud2/dualprovider1.p12 \
    certificates/testcloud2/truststore.p12 \
    127.0.0.1 8443 9000 1 &

java -jar target/*-jar-with-dependencies.jar \
    certificates/testcloud2/dualprovider2.p12 \
    certificates/testcloud2/truststore.p12 \
    127.0.0.1 8443 9004 2

