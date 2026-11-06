package com.dobby.price_alert.dto.nse.derivatives;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FiiIndexOiDto {

    private LocalDate date;

    // Original OI values
    private long futureIndexLong;
    private long futureIndexShort;

    private long optionIndexCallLong;
    private long optionIndexPutLong;

    private long optionIndexCallShort;
    private long optionIndexPutShort;

    // Original net values
    private long futuresNet;
    private long optionsNet;
    private long combinedNet;

    // Additional percentage values
    private double futuresLongPercentage;
    private double futuresShortPercentage;

    private double optionsLongPercentage;
    private double optionsShortPercentage;

    public FiiIndexOiDto(
            LocalDate date,
            long futureIndexLong,
            long futureIndexShort,
            long optionIndexCallLong,
            long optionIndexPutLong,
            long optionIndexCallShort,
            long optionIndexPutShort,
            long futuresNet,
            long optionsNet,
            long combinedNet,
            double futuresLongPercentage,
            double futuresShortPercentage,
            double optionsLongPercentage,
            double optionsShortPercentage) {

        this.date = date;

        this.futureIndexLong = futureIndexLong;
        this.futureIndexShort = futureIndexShort;

        this.optionIndexCallLong = optionIndexCallLong;
        this.optionIndexPutLong = optionIndexPutLong;

        this.optionIndexCallShort = optionIndexCallShort;
        this.optionIndexPutShort = optionIndexPutShort;

        this.futuresNet = futuresNet;
        this.optionsNet = optionsNet;
        this.combinedNet = combinedNet;

        this.futuresLongPercentage = futuresLongPercentage;
        this.futuresShortPercentage = futuresShortPercentage;

        this.optionsLongPercentage = optionsLongPercentage;
        this.optionsShortPercentage = optionsShortPercentage;
    }
}