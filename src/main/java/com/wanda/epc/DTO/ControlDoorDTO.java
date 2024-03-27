package com.wanda.epc.DTO;
import lombok.Data;

import java.util.List;

@Data
public class ControlDoorDTO {

    private Integer reason;
    private Integer type;
    private List<ControlDTO> controlDTOs;

}
