java -jar dual-provider/target/*-jar-with-dependencies.jar \
    config/properties/dualprovider1.properties &

dualProvider1=$!

java -jar dual-provider/target/*-jar-with-dependencies.jar \
    config/properties/dualprovider2.properties &

dualProvider2=$!

java -jar pde-tester/target/*-jar-with-dependencies.jar \
    config/properties/pde-tester.properties &

pdeTester=$!

wait $pdeTester

kill $dualProvider1
kill $dualProvider2