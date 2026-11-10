
package library.integration.soap.employee;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for departmentStat complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>
 * &lt;complexType name="departmentStat"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="averageSalary" type="{http://www.w3.org/2001/XMLSchema}double"/&gt;
 *         &lt;element name="count" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="department" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="totalSalary" type="{http://www.w3.org/2001/XMLSchema}double"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "departmentStat", propOrder = {
    "averageSalary",
    "count",
    "department",
    "totalSalary"
})
public class DepartmentStat {

    protected double averageSalary;
    protected int count;
    protected String department;
    protected double totalSalary;

    /**
     * Gets the value of the averageSalary property.
     * 
     */
    public double getAverageSalary() {
        return averageSalary;
    }

    /**
     * Sets the value of the averageSalary property.
     * 
     */
    public void setAverageSalary(double value) {
        this.averageSalary = value;
    }

    /**
     * Gets the value of the count property.
     * 
     */
    public int getCount() {
        return count;
    }

    /**
     * Sets the value of the count property.
     * 
     */
    public void setCount(int value) {
        this.count = value;
    }

    /**
     * Gets the value of the department property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDepartment() {
        return department;
    }

    /**
     * Sets the value of the department property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDepartment(String value) {
        this.department = value;
    }

    /**
     * Gets the value of the totalSalary property.
     * 
     */
    public double getTotalSalary() {
        return totalSalary;
    }

    /**
     * Sets the value of the totalSalary property.
     * 
     */
    public void setTotalSalary(double value) {
        this.totalSalary = value;
    }

}
