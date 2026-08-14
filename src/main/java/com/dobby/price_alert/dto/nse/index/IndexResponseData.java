package com.dobby.price_alert.dto.nse.index;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndexResponseData {

    List<IndexData> data;
}
