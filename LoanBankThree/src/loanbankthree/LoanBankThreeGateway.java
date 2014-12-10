package loanbankthree;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import utilities.xml.xmlMapper;

public class LoanBankThreeGateway implements IloanBankThreeGateway {

    private static final String EXCHANGE_NAME = "loan_bank_three";
    private static final String IN_QUEUE_NAME = "loan_bank_number_three";
    private static Bank bank = new Bank();

    public static void main(String[] args) throws IOException {

        IloanBankThreeGateway gateway = new LoanBankThreeGateway();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("nicklas");
        factory.setPassword("cph");
        factory.setHost("datdb.cphbusiness.dk");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(IN_QUEUE_NAME, false, false, false, null);
        channel.exchangeDeclare(EXCHANGE_NAME, "direct");
        channel.queueBind(IN_QUEUE_NAME, EXCHANGE_NAME, "");
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(IN_QUEUE_NAME, consumer);

        while (true) {
            QueueingConsumer.Delivery delivery = null;
            try {
                delivery = consumer.nextDelivery();
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                String message = new String(delivery.getBody());
                System.out.println("Message: " + message);
                String replyTo = delivery.getProperties().getReplyTo();
                BasicProperties prop = new BasicProperties().builder().correlationId(delivery.getProperties().getCorrelationId()).build();
                channel.basicPublish("", replyTo, prop, gateway.getBankReply(message).getBytes());
            } catch (InterruptedException ex) {
                System.out.println("Interupted" + Arrays.toString(ex.getStackTrace()));
            } catch (ShutdownSignalException ex) {
                System.out.println("ShutdownSigna" + Arrays.toString(ex.getStackTrace()));
            } catch (ConsumerCancelledException ex) {
                System.out.println("ConsumerCalledExecption" + Arrays.toString(ex.getStackTrace()));
            }
        }

    }

    @Override
    public String getBankReply(String msg) {

        float ReplyBank = 0.0f;
        String ssn = "";
        Document doc = xmlMapper.getXMLDocument(msg);

        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            ssn = xPath.compile("/LoanRequest/ssn").evaluate(doc);
            ReplyBank = bank.getInterestRate(Integer.parseInt(xPath.compile("/LoanRequest/creditScore").evaluate(doc)),
                    Integer.parseInt(xPath.compile("/LoanRequest/loanDuration").evaluate(doc)),
                    Double.parseDouble(xPath.compile("/LoanRequest/loanAmount").evaluate(doc)));
        } catch (XPathExpressionException ex) {
           ex.getStackTrace();
        }

        return getResultMessage(ReplyBank, ssn);
    }

  

    public String getResultMessage(float ReplyBank, String ssn) {
        String body = "";
        try {

            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Node loanResponse = doc.createElement("LoanResponse");
            loanResponse.appendChild(doc.createElement("ssn")).appendChild(doc.createTextNode(ssn));
            loanResponse.appendChild(doc.createElement("bankName")).appendChild(doc.createTextNode(bank.getBankName()));
            loanResponse.appendChild(doc.createElement("interestRate")).appendChild(doc.createTextNode(Float.toString(ReplyBank)));

            body = xmlMapper.getStringFromDoc(doc);
            System.out.println("reply" + body );
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        }
        return body;
    }

}
