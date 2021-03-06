package hotelreservation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import hotelreservation.config.kafka.KafkaProcessor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired PaymentRepository paymentRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReservationRegistered_PaymentRequestPolicy(@Payload ReservationRegistered reservationRegistered){

        if(!reservationRegistered.validate()) return;

        System.out.println("\n\n##### listener PaymentRequestPolicy : " + reservationRegistered.toJson() + "\n\n");

        // Sample Logic //
        Payment payment = new Payment();
        paymentRepository.save(payment);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReservationCanceled_PaymentCancelPolicy(@Payload ReservationCanceled reservationCanceled){

        if(!reservationCanceled.validate()) return;

        System.out.println("\n\n##### listener PaymentCancelPolicy : " + reservationCanceled.toJson() + "\n\n");

        // Sample Logic //
        Payment payment = new Payment();
        paymentRepository.save(payment);
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
