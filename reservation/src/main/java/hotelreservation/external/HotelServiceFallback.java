package hotelreservation.external;

import org.springframework.stereotype.Component;


@Component
public class HotelServiceFallback implements HotelService {

    // circuit breaker 발동 시 Hotel을 null값으로 리턴하며 "Circuit breaker has been opened. Fallback returned instead." 문구 출력
    @Override
    public Hotel getHotelStatus(Long id) {
        System.out.println("Circuit breaker has been opened. Fallback returned instead.");
        return false;
    }

}
