package com.airticket.controller;
import com.airticket.service.ItineraryPlanningService;
import com.airticket.util.CurrentUser;
import com.airticket.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/ai")
public class AiController {
 @Autowired private ItineraryPlanningService ai;
 @GetMapping("/capabilities") public Result<?> capabilities(){return Result.success(ai.capabilities());}
 @PostMapping("/itineraries/plan") public Result<?> plan(@RequestBody Map<String,String> body){return Result.success(ai.plan(CurrentUser.id(),body.get("message")));}
 @GetMapping("/itineraries/{id}") public Result<?> get(@PathVariable String id){return Result.success(ai.get(id,CurrentUser.id()));}
}
