
package library.integration.soap.employee;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getStatisticsResponse complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>
 * &lt;complexType name="getStatisticsResponse"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="statistics" type="{http://service.employee/}employeeStats" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getStatisticsResponse", propOrder = {
    "statistics"
})
public class GetStatisticsResponse {

    protected EmployeeStats statistics;

    /**
     * Gets the value of the statistics property.
     * 
     * @return
     *     possible object is
     *     {@link EmployeeStats }
     *     
     */
    public EmployeeStats getStatistics() {
        return statistics;
    }

    /**
     * Sets the value of the statistics property.
     * 
     * @param value
     *     allowed object is
     *     {@link EmployeeStats }
     *     
     */
    public void setStatistics(EmployeeStats value) {
        this.statistics = value;
    }

}
