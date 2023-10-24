import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CertificateInfoTest {

    public CertificateInfo certificateInfo;

    @BeforeEach
    void setUp(){
        CertificateInfo certificateInfo = new CertificateInfo();
    }

    @Test
    void isAddressValidTest_TRUE() {
        String str = "127.000.101.0/24";
        Assertions.assertTrue(CertificateInfo.isAddressValid(str));
    }
    @Test
    void isAddressValidTest_FALSE() {
        String str = "127.0f0.222.0/24";
        Assertions.assertFalse(CertificateInfo.isAddressValid(str));
    }
}