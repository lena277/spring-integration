package com.example.rssfeed.config;

import com.example.rssfeed.dto.ItemDTO;
import com.rometools.rome.feed.synd.SyndEntry;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
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
import org.springframework.integration.xml.transformer.MarshallingTransformer;
import org.springframework.integration.xml.transformer.ResultToDocumentTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Configuration
public class RssFeedHandler {

    @Value("${rssFeed.path}")
    private Resource feedResource;

    @Value("${outputDerictory.path}")
    private String outputDirecory;

    @Value("${logdDerictory.path}")
    private String logsDirecory;

    private String DEFAULT_PUBLISHED_DATE ="Wed May 08 14:00:36 IDT 2019";


    @Bean
    public IntegrationFlow feedFlow() {
        try{
            return IntegrationFlows

                    .from(Feed.inboundAdapter(this.feedResource, "rssFeed"),
                            e -> e.poller(p -> p.fixedRate(0).maxMessagesPerPoll(6)))

                    .<SyndEntry, ItemDTO> transform(
                            payload -> new ItemDTO(payload.getUri(),payload.getCategories().get(0).getName(),payload.getPublishedDate(),
                                    payload.getComments(), payload.getDescription().getValue(),payload.getLink())).

                            handle ((GenericHandler<ItemDTO>) ((p, h) -> {
                                Files.outboundAdapter(new File(outputDirecory+"/"+convertDate(p.getPubDate())+"/"+
                                                      p.getCategories()))
                                      .autoCreateDirectory(true);
                                return p;
                            }))
                    .channel(MessageChannels.executor(threadPoolTaskExecutor()))
                    .<ItemDTO, ItemDTO> transform(e -> {
                        try {
                            new MarshallingTransformer(jaxbMarshaller(outputDirecory+"/"+convertDate(e.getPubDate())+"/"+
                                    e.getCategories()), new ResultToDocumentTransformer());

                        } catch (ParserConfigurationException ex) {
                            ex.printStackTrace();
                        }
                        return e;
                    })
                     .log(LoggingHandler.Level.DEBUG,
                                m -> m.getHeaders().getId())
                     .handle(e -> targetLogs())
                     .get();
                    }
        catch (Exception e){
        }
   return null;
    }

    public Marshaller jaxbMarshaller(String p) {

        StaxEventItemWriter<ItemDTO> xmlFileWriter = new StaxEventItemWriter<>();
        xmlFileWriter.setResource(new FileSystemResource(p));
        xmlFileWriter.setRootTagName("items");
        Map<String, Object> props = new HashMap<>();
        props.put(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        props.put(javax.xml.bind.Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        props.put(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8");

        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setMarshallerProperties(props);
        jaxb2Marshaller.setPackagesToScan("com.example.rssfeed.dto");
        xmlFileWriter.setMarshaller(jaxb2Marshaller);
        return jaxb2Marshaller;
    }

    @Bean
    public TaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(6);
        executor.setCorePoolSize(5);
        executor.setKeepAliveSeconds(300);
        return executor;
    }


    @Bean
    public MessageHandler targetDirectory() {
        return new MessageHandler() {
            @Override
            public void handleMessage(Message<?> message) throws MessagingException {
                FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(outputDirecory));
                handler.setFileNameGenerator( message2 -> message.getHeaders().getId()+".xml");
                handler.setAutoCreateDirectory(true);
                handler.setTemporaryFileSuffix(".xml");
                handler.setFileExistsMode(FileExistsMode.REPLACE);
            }
        };
    }

    @Bean
    public MessageHandler targetLogs() {

        return new MessageHandler() {
            @Override
            public void handleMessage(Message<?> message) throws MessagingException {
                FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(logsDirecory));
                handler.setFileNameGenerator( message2 -> message.getHeaders().getId()+".log");
                handler.setTemporaryFileSuffix(".log");
                handler.setAppendNewLine(true);
                handler.setAutoCreateDirectory(true);


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
