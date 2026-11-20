package com.airticket.vo;

import java.util.List;

public class PageResult<T> {
    private Long total;
    private List<T> records;
    private Integer page;
    private Integer pageSize;

    public PageResult() {
    }

    public PageResult(Long total, List<T> records, Integer page, Integer pageSize) {
        this.total = total;
        this.records = records;
        this.page = page;
        this.pageSize = pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}