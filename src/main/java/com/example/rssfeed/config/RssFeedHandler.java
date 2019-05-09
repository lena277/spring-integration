package com.example.rssfeed.config;

import com.example.rssfeed.model.Items;
import com.rometools.rome.feed.synd.SyndEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.feed.dsl.Feed;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.integration.transformer.AbstractPayloadTransformer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executor;

@Configuration
public class RssFeedHandler {

    @Value("${rssFeed.path}")
    private Resource feedResource;

    @Value("${outputDerictory.path}")
    private String outputDirecory;

    @Value("${metaDataStore.path}")
    private String metaDataStorePath;

    private File tempFolder;
    private String DEFAULT_PUBLISHED_DATE ="Wed May 08 14:00:36 IDT 2019";
    @Bean
    public MetadataStore metadataStore() {
        PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
        tempFolder = new File(metaDataStorePath);
        metadataStore.setBaseDirectory(tempFolder.getAbsolutePath());
        return metadataStore;
    }
    @Bean("exeutor")
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
    public IntegrationFlow feedFlow() {
        return IntegrationFlows
                .from(Feed.inboundAdapter(this.feedResource, "rssFeed")
                                .metadataStore(metadataStore()),
                        e -> e.poller(p -> p.fixedDelay(10).maxMessagesPerPoll(5))).
                        channel(MessageChannels.executor(threadPoolTaskExecutor())).
                        <SyndEntry, Items> transform(

                                payload -> new Items(payload.getAuthor(),payload.getCategories(),payload.getPublishedDate(),
                                        payload.getComments(), payload.getDescription(),payload.getLink())).

                        handle(e ->
                                Files.outboundAdapter(new File(outputDirecory+"/"+convertDate(((Items)e.getPayload()).getPubDate())+
                                        ((Items)e.getPayload()).getCategories().get(0).getName()))
                                        .temporaryFileSuffix(".xml").
                                        autoCreateDirectory(true)
                                        .appendNewLine(true)
                                        .fileNameGenerator( message -> outputDirecory+"/"+convertDate(((Items)e.getPayload()).getPubDate())+
                                                ((Items)e.getPayload()).getCategories().get(0).getName() +"/"+ e.getHeaders().getId().toString()).get())
//                         wireTap(sf -> sf.log(LoggingHandler.Level.DEBUG, "Log ",
//                m -> m.getHeaders().getId() + ": " + m.getPayload())).bridge()
                        .get();

    }
    @Bean
    public TaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(5);
        executor.setCorePoolSize(5);
        executor.setQueueCapacity(22);
        return executor;
    }


    @Bean
    public MessageHandler targetDirectory() {
        FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(outputDirecory));
        handler.getExpression();
        handler.setFileExistsMode(FileExistsMode.REPLACE);
        handler.setExpectReply(false);
        return handler;
    }

    public String convertDate(Date publishedDate){
        String initialDate = DEFAULT_PUBLISHED_DATE;
        if( publishedDate !=null){
            initialDate =  publishedDate.toString();}

        DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy");
        Calendar cal = Calendar.getInstance();

        try {
            Date date = (Date) formatter.parse(initialDate);
            cal.setTime(date);
        }catch (ParseException e){

        }

        return cal.get(Calendar.DATE) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.YEAR);

    }


}
