
package hotelreservation.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Date;

@FeignClient(name="hotel", url="${feign.hotel.url}")
public interface HotelService {

    @RequestMapping(method= RequestMethod.GET, value="/hotels/{id}", consumes = "application/json")
    public Hotel getHotelStatus(@PathVariable("id") Long id);

}
