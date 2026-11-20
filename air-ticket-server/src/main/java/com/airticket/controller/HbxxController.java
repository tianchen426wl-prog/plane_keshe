package com.airticket.controller;

import com.airticket.entity.Hbxx;
import com.airticket.entity.HbxxCabin;
import com.airticket.service.HbxxCabinService;
import com.airticket.service.HbxxService;
import com.airticket.vo.PageResult;
import com.airticket.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hbxx")
public class HbxxController {

    @Autowired
    private HbxxService hbxxService;

    @Autowired
    private HbxxCabinService hbxxCabinService;

    @GetMapping("/search")
    public Result<PageResult<Hbxx>> search(@RequestParam(required = false) String start,
                                           @RequestParam(required = false) String end,
                                           @RequestParam(required = false) String starttime,
                                           @RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<Hbxx> result = hbxxService.search(start, end, starttime, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    public Result<Hbxx> detail(@PathVariable Integer id) {
        Hbxx hbxx = hbxxService.getById(id);
        return Result.success(hbxx);
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody Hbxx hbxx) {
        hbxxService.add(hbxx);
        return Result.success(null);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody Hbxx hbxx) {
        hbxxService.update(hbxx);
        return Result.success(null);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        hbxxService.delete(id);
        return Result.success(null);
    }

    @GetMapping("/{hbid}/cabins")
    public Result<List<HbxxCabin>> cabins(@PathVariable Integer hbid) {
        List<HbxxCabin> cabins = hbxxCabinService.getCabinsByHbid(hbid);
        return Result.success(cabins);
    }
}
