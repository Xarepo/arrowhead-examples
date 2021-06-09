java -jar temperature-provider/target/*-jar-with-dependencies.jar \
    config/properties/temperature-provider-1.properties &

tempProvider1=$!

java -jar temperature-provider/target/*-jar-with-dependencies.jar \
    config/properties/temperature-provider-2.properties &

tempProvider2=$!

java -jar fan/target/*-jar-with-dependencies.jar \
    config/properties/fan.properties &

fan=$!

wait $fan

kill $tempProvider1
kill $tempProvider2