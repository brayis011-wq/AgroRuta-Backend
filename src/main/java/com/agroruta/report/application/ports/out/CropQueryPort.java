package com.agroruta.report.application.ports.out;

import com.agroruta.report.domain.CropDetail;
import java.util.List;

public interface CropQueryPort {
    List<CropDetail> getAllCropDetails();
}