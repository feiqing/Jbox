package com.github.jbox.h2;

import com.github.jbox.serializer.support.Hessian2Serializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/9/24 2:55 PM.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Index(name = "idx_id_name", fields = {"id", "name"}, unique = true)
@Index(name = "idx_gmt_create", fields = "gmt_create", unique = false)
@NoArgsConstructor
public class TestModel extends H2BaseMode {

    private static final long serialVersionUID = -5089154080396442891L;

    private String name;

    private byte bytes;

    private Short Shorts;

    private int ints;

    private long longs;

    private Float Floats;

    private double doubles;

    private Boolean Booleans;

    private char chars;

    private Character Characters;

    private BigDecimal BigDecimals;

    private LocalTime localTimes;

    private LocalDate LocalDates;

    @Column(serializer = MySerializer.class)
    private LocalDateTime LocalDateTimes;

    @Column(serializer = Hessian2Serializer.class)
    public TestModel model;
}
