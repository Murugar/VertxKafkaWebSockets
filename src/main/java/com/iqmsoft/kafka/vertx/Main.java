package com.iqmsoft.kafka.vertx;

import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;


public class Main extends AbstractVerticle {
	
	
	   public void scheduleConsume() {
	        vertx.executeBlocking(future -> {
	            consume();
	            future.complete();
	        }, res -> {
	            System.out.println("Done");
	            scheduleConsume();
	        });
	    }

	    public void consume() {
	        LoggerFactory.getLogger("MyLog").info("Kafka Consuming...");

	        ConsumerRecords<String, String> records = consumer.poll(1000);
	        for (ConsumerRecord<String, String> record : records) {
	            System.out.printf("offset = %d, key = %s, value = %s", record.offset(), record.key(), record.value());
	            vertx.eventBus().publish("news-feed", record.value());
	        }
	    }
	    
	
	private org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Main());

      
    }

    @Override
    public void start() throws Exception {
    	
    	  Properties props = new Properties();
          props.put("bootstrap.servers", "localhost:9092");
          props.put("group.id", "VertxKafkaConsumer");
          props.put("enable.auto.commit", "true");
          props.put("auto.commit.interval.ms", "1000");
          props.put("session.timeout.ms", "30000");
          props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
          props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
          consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<String, String>(props);
          consumer.subscribe(Arrays.asList("GuidanceEvents"));
    	
          scheduleConsume();
    	
    	
        Router router = Router.router(vertx);

        BridgeOptions options = new BridgeOptions().addOutboundPermitted(new PermittedOptions().setAddress("news-feed"));

        router.route("/eventbus/*").handler(SockJSHandler.create(vertx).bridge(options, event -> {

            // You can also optionally provide a handler like this which will be passed any events that occur on the bridge
            // You can use this for monitoring or logging, or to change the raw messages in-flight.
            // It can also be used for fine grained access control.

            if (event.type() == BridgeEventType.SOCKET_CREATED) {
                System.out.println("A socket was created");
            }

            // This signals that it's ok to process the event
            event.complete(true);

        }));

        // Serve the static resources
        router.route().handler(StaticHandler.create());

        vertx.createHttpServer().requestHandler(router::accept).listen(8082);


        

    }
}
