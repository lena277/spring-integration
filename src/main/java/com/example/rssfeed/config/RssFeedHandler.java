package com.example.rssfeed.config;

import com.example.rssfeed.model.Items;
import com.rometools.rome.feed.rss.Item;
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
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
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

    @Value("${logdDerictory.path}")
    private String logsDirecory;

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

    @Bean
    public IntegrationFlow feedFlow() {
        return IntegrationFlows
                .from(Feed.inboundAdapter(this.feedResource, "rssFeed")
                                .metadataStore(metadataStore()),
                        e -> e.poller(p -> p.fixedDelay(10).maxMessagesPerPoll(6)))

                .channel(MessageChannels.executor(threadPoolTaskExecutor()))
                .<SyndEntry, Items> transform(
                        payload -> new Items(payload.getAuthor(),payload.getCategories(),payload.getPublishedDate(),
                                payload.getComments(), payload.getDescription(),payload.getLink())).

                        handle ((GenericHandler<Items >) ((p, h) -> {
                            Files.outboundAdapter(new File(outputDirecory+"/"+convertDate(p.getPubDate())+
                                    p.getCategories().get(0).getName()))
                            .temporaryFileSuffix(".xml")
                            .autoCreateDirectory(true)
                            .appendNewLine(true)
                            .fileNameGenerator(message -> h.getId()+".xml");
                            return p;
                        }))
                .handle((GenericHandler< Items >)( (p,h) ->{
                    targetDirectory();
                    return p;
                }))
                .log(LoggingHandler.Level.DEBUG,
                        m -> m.getHeaders().getId() + ": " + m.getPayload())
                .handle(targetLogs())
                .get();
    }

    @Bean
    public TaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(5);
        executor.setCorePoolSize(5);
        executor.setQueueCapacity(10);
        return executor;
    }


    @Bean
    public MessageHandler targetDirectory() {
        return new MessageHandler() {
            @Override
            public void handleMessage(Message<?> message) throws MessagingException {
                FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(outputDirecory+"/"+convertDate(((Items)message.getPayload()).getPubDate())+
                        ((Items)message.getPayload()).getCategories().get(0).getName()));
                handler.getExpression();
                handler.setFileNameGenerator( message2 -> message.getHeaders().getId()+".xml");
                handler.setTemporaryFileSuffix(".xml");
                handler.setFileExistsMode(FileExistsMode.REPLACE);
                handler.setExpectReply(false);
            }
        };
    }
    @Bean
    public MessageHandler targetLogs() {
        return new MessageHandler() {

            @Override
            public void handleMessage(Message<?> message) throws MessagingException {
                FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(logsDirecory));
                handler.getExpression();
                handler.setFileNameGenerator( message2 -> message.getHeaders().getId()+".log");
                handler.setTemporaryFileSuffix(".log");
                handler.setAppendNewLine(true);
                handler.setAutoCreateDirectory(true);
                handler.setExpectReply(false);

            }
        };
    }

    public String convertDate(Date publishedDate){
        String initialDate = DEFAULT_PUBLISHED_DATE;
        if( publishedDate !=null){
            initialDate =  publishedDate.toString();}

        DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy");
        Calendar cal = Calendar.getInstance();
        try {
            Date date = formatter.parse(initialDate);
            cal.setTime(date);
        }catch (ParseException e){
        }
        return cal.get(Calendar.DATE) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.YEAR);
    }
}
