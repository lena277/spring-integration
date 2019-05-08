package com.example.rssfeed;

import com.rometools.rome.feed.rss.Item;
import com.rometools.rome.feed.synd.SyndEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.feed.dsl.Feed;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.splitter.FileSplitter;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.integration.transformer.AbstractPayloadTransformer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.zip.DataFormatException;

@SpringBootApplication
@Configuration
public class RssfeedApplication {

    public static void main(String[] args) {
        SpringApplication.run(RssfeedApplication.class, args);
    }

    @Value("${rssFeed.path}")
    private Resource feedResource;

    @Value("${outputDerictory.path}")
    private String outputDirecory;

    public File tempFolder = new File("");

    @Bean
    public MetadataStore metadataStore() {
        PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
        metadataStore.setBaseDirectory(tempFolder.getAbsolutePath());
        return metadataStore;
    }
    public Executor getExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(20);
        executor.initialize();
        return executor;
    }
    @Bean
    public MessageChannel queueChannel() {
        return MessageChannels.queue().get();
    }

    @Bean
    public IntegrationFlow feedFlow() {
        return IntegrationFlows
                .from(Feed.inboundAdapter(this.feedResource, "rssFeed")
                                .metadataStore(metadataStore()),
                        e -> e.poller(p -> p.fixedDelay(100))).transform(extractLinkFromFeed()).handle(targetDirectory()).log(LoggingHandler.Level.DEBUG, "TEST_LOGGER",
                        m -> m.getHeaders().getId() + ": " + m.getPayload()).get();
    }
    @Bean
    public MessageHandler targetDirectory() {
        FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(outputDirecory));
        handler.getExpression();
        handler.setFileExistsMode(FileExistsMode.REPLACE);
        handler.setExpectReply(false);
        return handler;
    }

     public String convertDate(SyndEntry payload){
         String dateStr = "Wed May 08 14:00:36 IDT 2019"; //default
         if( payload.getPublishedDate()!=null){
             dateStr =  payload.getPublishedDate().toString();}

         DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy");
         Calendar cal = Calendar.getInstance();

         try {
             Date date = (Date) formatter.parse(dateStr);
             cal.setTime(date);
         }catch (ParseException e){

         }

        return cal.get(Calendar.DATE) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.YEAR);

     }

    @Bean
    public AbstractPayloadTransformer<SyndEntry, String> extractLinkFromFeed() {
        return new AbstractPayloadTransformer<SyndEntry, String>() {
            @Override
            protected String transformPayload(SyndEntry payload) throws Exception {
                String formatedDate = convertDate(payload);
                createSubDir(formatedDate,payload.getCategories().get(0).getName() );
                return payload.getComments();
            }

        };

    }

    public void createDir(String name){
        if(!name.startsWith(outputDirecory))
            name =  outputDirecory+"/"+ name;


        File theDir = new File(name);

        if (!theDir.exists()) {
            try{
                theDir.mkdir();
            }
            catch(SecurityException se){
                //handle it
            }

        }

    }
    public void createSubDir(String name, String sub){
        File subDir = null;
        if(!new File(outputDirecory+"/"+name+"/"+name).exists())
            createDir(name);

        subDir = new File(outputDirecory+"/"+name+"/"+sub);

        if (!subDir.exists()) {

            try{
                subDir.mkdir();
            }
            catch(SecurityException se){
                //handle it
            }
        }

    }

}
