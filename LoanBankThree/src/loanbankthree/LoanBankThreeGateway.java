package loanbankthree;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import dk.cphbusiness.connection.ConnectionCreator;
import java.io.IOException;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import utilities.xml.xmlMapper;

public class LoanBankThreeGateway implements IloanBankThreeGateway {

    private static final String IN_QUEUE_NAME = "bank_three_gr1";
    private static final Bank bank = new Bank();

    public static void main(String[] args) throws IOException {

        IloanBankThreeGateway gateway = new LoanBankThreeGateway();
        ConnectionCreator creator = ConnectionCreator.getInstance();
        Channel channel = creator.createChannel();
        channel.queueDeclare(IN_QUEUE_NAME, false, false, false, null);
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
            Element loanResponse = doc.createElement("LoanResponse");
            doc.appendChild(loanResponse);

            Element ssnE = doc.createElement("ssn");
            ssnE.appendChild(doc.createTextNode(ssn));
            Element bankName = doc.createElement("bankName");
            bankName.appendChild(doc.createTextNode(bank.getBankName()));
            Element interestRate = doc.createElement("intrestRate");
            interestRate.appendChild(doc.createTextNode(Float.toString(ReplyBank)));
            loanResponse.appendChild(ssnE);
            loanResponse.appendChild(bankName);
            loanResponse.appendChild(interestRate);

            body = xmlMapper.getStringFromDoc(doc);
            System.out.println("reply " + body);
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        }
        return body;
    }

}
