package offercheck.dto;

import lombok.Data;

@Data
public class OfferRequest {
    private String role;
    private String company;
    private String location;
    private int yearsOfExperience;
    private double baseSalary;
    private double bonus;
    private double equity;
    private String benefits;
    private String level;
}
