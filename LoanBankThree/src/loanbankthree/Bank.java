package loanbankthree;

public class Bank {
   
    private static final float BASE_RATE = 2.0f;
    private String bankName;
    
    public Bank(){
        bankName = "Bank3";
    }
    

    public String getBankName() {
        return bankName;
    }

    public Float getInterestRate(int creditScore, int loanDuration, double amount){
        float credit = creditScore;
        float result = BASE_RATE + ((801 - credit)/100);
        if(amount < 10000){
            result += 0.3f;
        }else if(amount < 100000){
            result += 0.18f;
        }else
            result += 0.6f;
        if(loanDuration < 180){
            result += 3.0f;
        }else if(loanDuration < 720){
            result += 1.4f;
        }else
            result += 4.0f;
        return result;
    }
    
}
