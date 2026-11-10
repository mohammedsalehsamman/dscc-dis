
package library.integration.soap.employee;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for employeeStats complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>
 * &lt;complexType name="employeeStats"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="averageSalary" type="{http://www.w3.org/2001/XMLSchema}double"/&gt;
 *         &lt;element name="count" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="departments" type="{http://service.employee/}departmentStat" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="maxSalary" type="{http://www.w3.org/2001/XMLSchema}double"/&gt;
 *         &lt;element name="minSalary" type="{http://www.w3.org/2001/XMLSchema}double"/&gt;
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
@XmlType(name = "employeeStats", propOrder = {
    "averageSalary",
    "count",
    "departments",
    "maxSalary",
    "minSalary",
    "totalSalary"
})
public class EmployeeStats {

    protected double averageSalary;
    protected int count;
    @XmlElement(nillable = true)
    protected List<DepartmentStat> departments;
    protected double maxSalary;
    protected double minSalary;
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
     * Gets the value of the departments property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the departments property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getDepartments().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DepartmentStat }
     * </p>
     * 
     * 
     * @return
     *     The value of the departments property.
     */
    public List<DepartmentStat> getDepartments() {
        if (departments == null) {
            departments = new ArrayList<DepartmentStat>();
        }
        return this.departments;
    }

    /**
     * Gets the value of the maxSalary property.
     * 
     */
    public double getMaxSalary() {
        return maxSalary;
    }

    /**
     * Sets the value of the maxSalary property.
     * 
     */
    public void setMaxSalary(double value) {
        this.maxSalary = value;
    }

    /**
     * Gets the value of the minSalary property.
     * 
     */
    public double getMinSalary() {
        return minSalary;
    }

    /**
     * Sets the value of the minSalary property.
     * 
     */
    public void setMinSalary(double value) {
        this.minSalary = value;
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
