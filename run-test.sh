java -jar dual-provider/target/*-jar-with-dependencies.jar \
    config/certificates/dualprovider1.p12 \
    config/certificates/truststore.p12 \
    127.0.0.1 8443 9000 1 &

java -jar dual-provider/target/*-jar-with-dependencies.jar \
    config/certificates/dualprovider2.p12 \
    config/certificates/truststore.p12 \
    127.0.0.1 8443 9004 2 &

java -jar pde-tester/target/*-jar-with-dependencies.jar