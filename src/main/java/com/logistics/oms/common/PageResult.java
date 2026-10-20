package com.logistics.oms.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页返回体。
 */
@Data
public class PageResult<T> {

    private long current;
    private long size;
    private long total;
    private long pages;
    private List<T> records;

    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> r = new PageResult<>();
        r.current = page.getCurrent();
        r.size = page.getSize();
        r.total = page.getTotal();
        r.pages = page.getPages();
        r.records = page.getRecords();
        return r;
    }

    /** 分页结果整体映射（用于 entity -> VO）。 */
    public static <S, T> PageResult<T> of(Page<S> page, Function<S, T> mapper) {
        PageResult<T> r = new PageResult<>();
        r.current = page.getCurrent();
        r.size = page.getSize();
        r.total = page.getTotal();
        r.pages = page.getPages();
        r.records = page.getRecords().stream().map(mapper).collect(Collectors.toList());
        return r;
    }
}
