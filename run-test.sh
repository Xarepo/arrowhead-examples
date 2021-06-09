java -jar dual-provider/target/*-jar-with-dependencies.jar \
    config/properties/dual-provider-1.properties &

dualProvider1=$!

java -jar dual-provider/target/*-jar-with-dependencies.jar \
    config/properties/dual-provider-2.properties &

dualProvider2=$!

java -jar pde-tester/target/*-jar-with-dependencies.jar \
    config/properties/pde-tester.properties &

pdeTester=$!

wait $pdeTester

kill $dualProvider1
kill $dualProvider2