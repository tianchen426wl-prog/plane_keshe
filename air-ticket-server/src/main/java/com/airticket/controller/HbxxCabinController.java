package com.airticket.controller;

import com.airticket.entity.HbxxCabin;
import com.airticket.service.HbxxCabinService;
import com.airticket.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hbxx/cabin")
public class HbxxCabinController {

    @Autowired
    private HbxxCabinService hbxxCabinService;

    @GetMapping("/list/{hbid}")
    public Result<List<HbxxCabin>> list(@PathVariable Integer hbid) {
        List<HbxxCabin> cabins = hbxxCabinService.getCabinsByHbid(hbid);
        return Result.success(cabins);
    }

    @GetMapping("/detail/{cabinId}")
    public Result<HbxxCabin> detail(@PathVariable Integer cabinId) {
        HbxxCabin cabin = hbxxCabinService.getById(cabinId);
        return Result.success(cabin);
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody HbxxCabin cabin) {
        hbxxCabinService.add(cabin);
        return Result.success(null);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody HbxxCabin cabin) {
        hbxxCabinService.update(cabin);
        return Result.success(null);
    }

    @DeleteMapping("/delete/{cabinId}")
    public Result<Void> delete(@PathVariable Integer cabinId) {
        hbxxCabinService.deleteById(cabinId);
        return Result.success(null);
    }
}
