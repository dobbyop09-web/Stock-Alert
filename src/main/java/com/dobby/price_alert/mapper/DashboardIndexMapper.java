package com.dobby.price_alert.mapper;

import com.dobby.price_alert.dto.nse.DashboardIndexData;
import com.dobby.price_alert.dto.nse.IndexData;

public class DashboardIndexMapper {

    private DashboardIndexMapper() {
    }

    public static DashboardIndexData map(IndexData data) {

        if (data == null) {
            return null;
        }

        return DashboardIndexData.builder()

                .name(data.getIndex())

                .last(safeDouble(data.getLast()))
                .variation(safeDouble(data.getVariation()))
                .percentChange(safeDouble(data.getPercentChange()))

                .previousClose(
                        safeDouble(data.getPreviousClose())
                )

                .oneWeekChange(
                        calculateChange(
                                data.getLast(),
                                data.getOneWeekAgoVal()
                        )
                )

                .oneMonthChange(
                        calculateChange(
                                data.getLast(),
                                data.getOneMonthAgoVal()
                        )
                )

                .oneYearChange(
                        calculateChange(
                                data.getLast(),
                                data.getOneYearAgoVal()
                        )
                )

                .pe(
                        parseDouble(data.getPe())
                )

                .pb(
                        parseDouble(data.getPb())
                )

                .advances(
                        parseInt(data.getAdvances())
                )

                .declines(
                        parseInt(data.getDeclines())
                )

                .unchanged(
                        parseInt(data.getUnchanged())
                )
                .build();
    }


    private static double calculateChange(
            Double current,
            Double previous
    ) {

        if (current == null || previous == null || previous == 0) {
            return 0.0;
        }

        double result =
                ((current - previous) / previous) * 100.0;

        return round(result);
    }


    private static double parseDouble(String value) {

        if (value == null ||
                value.isBlank() ||
                value.equals("-")) {

            return 0.0;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }


    private static int parseInt(String value) {

        if (value == null ||
                value.isBlank() ||
                value.equals("-")) {

            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    private static double safeDouble(Double value) {

        return value == null ? 0.0 : value;
    }


    private static double round(double value) {

        return Math.round(value * 100.0) / 100.0;
    }
}