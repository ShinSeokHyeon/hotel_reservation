package hotelreservation.external;

import org.springframework.stereotype.Component;


@Component
public class HotelServiceFallback implements HotelService {

    @Override
    public Hotel getHotelStatus(Long id) {
        System.out.println("Circuit breaker has been opened. Fallback returned instead.");
        return null;
    }

}
