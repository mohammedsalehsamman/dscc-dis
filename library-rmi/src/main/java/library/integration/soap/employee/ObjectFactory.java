
package library.integration.soap.employee;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the library.integration.soap.employee package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _AddEmployee_QNAME = new QName("http://service.employee/", "addEmployee");
    private static final QName _AddEmployeeResponse_QNAME = new QName("http://service.employee/", "addEmployeeResponse");
    private static final QName _ApplyRaise_QNAME = new QName("http://service.employee/", "applyRaise");
    private static final QName _ApplyRaiseResponse_QNAME = new QName("http://service.employee/", "applyRaiseResponse");
    private static final QName _DeleteEmployee_QNAME = new QName("http://service.employee/", "deleteEmployee");
    private static final QName _DeleteEmployeeResponse_QNAME = new QName("http://service.employee/", "deleteEmployeeResponse");
    private static final QName _Employee_QNAME = new QName("http://service.employee/", "employee");
    private static final QName _GetActivitySince_QNAME = new QName("http://service.employee/", "getActivitySince");
    private static final QName _GetActivitySinceResponse_QNAME = new QName("http://service.employee/", "getActivitySinceResponse");
    private static final QName _GetDepartments_QNAME = new QName("http://service.employee/", "getDepartments");
    private static final QName _GetDepartmentsResponse_QNAME = new QName("http://service.employee/", "getDepartmentsResponse");
    private static final QName _GetEmployee_QNAME = new QName("http://service.employee/", "getEmployee");
    private static final QName _GetEmployeeResponse_QNAME = new QName("http://service.employee/", "getEmployeeResponse");
    private static final QName _GetStatistics_QNAME = new QName("http://service.employee/", "getStatistics");
    private static final QName _GetStatisticsResponse_QNAME = new QName("http://service.employee/", "getStatisticsResponse");
    private static final QName _ListEmployees_QNAME = new QName("http://service.employee/", "listEmployees");
    private static final QName _ListEmployeesResponse_QNAME = new QName("http://service.employee/", "listEmployeesResponse");
    private static final QName _SearchEmployees_QNAME = new QName("http://service.employee/", "searchEmployees");
    private static final QName _SearchEmployeesResponse_QNAME = new QName("http://service.employee/", "searchEmployeesResponse");
    private static final QName _UpdateEmployee_QNAME = new QName("http://service.employee/", "updateEmployee");
    private static final QName _UpdateEmployeeResponse_QNAME = new QName("http://service.employee/", "updateEmployeeResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: library.integration.soap.employee
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link AddEmployee }
     * 
     */
    public AddEmployee createAddEmployee() {
        return new AddEmployee();
    }

    /**
     * Create an instance of {@link AddEmployeeResponse }
     * 
     */
    public AddEmployeeResponse createAddEmployeeResponse() {
        return new AddEmployeeResponse();
    }

    /**
     * Create an instance of {@link ApplyRaise }
     * 
     */
    public ApplyRaise createApplyRaise() {
        return new ApplyRaise();
    }

    /**
     * Create an instance of {@link ApplyRaiseResponse }
     * 
     */
    public ApplyRaiseResponse createApplyRaiseResponse() {
        return new ApplyRaiseResponse();
    }

    /**
     * Create an instance of {@link DeleteEmployee }
     * 
     */
    public DeleteEmployee createDeleteEmployee() {
        return new DeleteEmployee();
    }

    /**
     * Create an instance of {@link DeleteEmployeeResponse }
     * 
     */
    public DeleteEmployeeResponse createDeleteEmployeeResponse() {
        return new DeleteEmployeeResponse();
    }

    /**
     * Create an instance of {@link Employee }
     * 
     */
    public Employee createEmployee() {
        return new Employee();
    }

    /**
     * Create an instance of {@link GetActivitySince }
     * 
     */
    public GetActivitySince createGetActivitySince() {
        return new GetActivitySince();
    }

    /**
     * Create an instance of {@link GetActivitySinceResponse }
     * 
     */
    public GetActivitySinceResponse createGetActivitySinceResponse() {
        return new GetActivitySinceResponse();
    }

    /**
     * Create an instance of {@link GetDepartments }
     * 
     */
    public GetDepartments createGetDepartments() {
        return new GetDepartments();
    }

    /**
     * Create an instance of {@link GetDepartmentsResponse }
     * 
     */
    public GetDepartmentsResponse createGetDepartmentsResponse() {
        return new GetDepartmentsResponse();
    }

    /**
     * Create an instance of {@link GetEmployee }
     * 
     */
    public GetEmployee createGetEmployee() {
        return new GetEmployee();
    }

    /**
     * Create an instance of {@link GetEmployeeResponse }
     * 
     */
    public GetEmployeeResponse createGetEmployeeResponse() {
        return new GetEmployeeResponse();
    }

    /**
     * Create an instance of {@link GetStatistics }
     * 
     */
    public GetStatistics createGetStatistics() {
        return new GetStatistics();
    }

    /**
     * Create an instance of {@link GetStatisticsResponse }
     * 
     */
    public GetStatisticsResponse createGetStatisticsResponse() {
        return new GetStatisticsResponse();
    }

    /**
     * Create an instance of {@link ListEmployees }
     * 
     */
    public ListEmployees createListEmployees() {
        return new ListEmployees();
    }

    /**
     * Create an instance of {@link ListEmployeesResponse }
     * 
     */
    public ListEmployeesResponse createListEmployeesResponse() {
        return new ListEmployeesResponse();
    }

    /**
     * Create an instance of {@link SearchEmployees }
     * 
     */
    public SearchEmployees createSearchEmployees() {
        return new SearchEmployees();
    }

    /**
     * Create an instance of {@link SearchEmployeesResponse }
     * 
     */
    public SearchEmployeesResponse createSearchEmployeesResponse() {
        return new SearchEmployeesResponse();
    }

    /**
     * Create an instance of {@link UpdateEmployee }
     * 
     */
    public UpdateEmployee createUpdateEmployee() {
        return new UpdateEmployee();
    }

    /**
     * Create an instance of {@link UpdateEmployeeResponse }
     * 
     */
    public UpdateEmployeeResponse createUpdateEmployeeResponse() {
        return new UpdateEmployeeResponse();
    }

    /**
     * Create an instance of {@link ActivityEntry }
     * 
     */
    public ActivityEntry createActivityEntry() {
        return new ActivityEntry();
    }

    /**
     * Create an instance of {@link EmployeeStats }
     * 
     */
    public EmployeeStats createEmployeeStats() {
        return new EmployeeStats();
    }

    /**
     * Create an instance of {@link DepartmentStat }
     * 
     */
    public DepartmentStat createDepartmentStat() {
        return new DepartmentStat();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddEmployee }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AddEmployee }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "addEmployee")
    public JAXBElement<AddEmployee> createAddEmployee(AddEmployee value) {
        return new JAXBElement<AddEmployee>(_AddEmployee_QNAME, AddEmployee.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddEmployeeResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AddEmployeeResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "addEmployeeResponse")
    public JAXBElement<AddEmployeeResponse> createAddEmployeeResponse(AddEmployeeResponse value) {
        return new JAXBElement<AddEmployeeResponse>(_AddEmployeeResponse_QNAME, AddEmployeeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ApplyRaise }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ApplyRaise }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "applyRaise")
    public JAXBElement<ApplyRaise> createApplyRaise(ApplyRaise value) {
        return new JAXBElement<ApplyRaise>(_ApplyRaise_QNAME, ApplyRaise.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ApplyRaiseResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ApplyRaiseResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "applyRaiseResponse")
    public JAXBElement<ApplyRaiseResponse> createApplyRaiseResponse(ApplyRaiseResponse value) {
        return new JAXBElement<ApplyRaiseResponse>(_ApplyRaiseResponse_QNAME, ApplyRaiseResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeleteEmployee }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link DeleteEmployee }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "deleteEmployee")
    public JAXBElement<DeleteEmployee> createDeleteEmployee(DeleteEmployee value) {
        return new JAXBElement<DeleteEmployee>(_DeleteEmployee_QNAME, DeleteEmployee.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeleteEmployeeResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link DeleteEmployeeResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "deleteEmployeeResponse")
    public JAXBElement<DeleteEmployeeResponse> createDeleteEmployeeResponse(DeleteEmployeeResponse value) {
        return new JAXBElement<DeleteEmployeeResponse>(_DeleteEmployeeResponse_QNAME, DeleteEmployeeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Employee }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link Employee }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "employee")
    public JAXBElement<Employee> createEmployee(Employee value) {
        return new JAXBElement<Employee>(_Employee_QNAME, Employee.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetActivitySince }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link GetActivitySince }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "getActivitySince")
    public JAXBElement<GetActivitySince> createGetActivitySince(GetActivitySince value) {
        return new JAXBElement<GetActivitySince>(_GetActivitySince_QNAME, GetActivitySince.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetActivitySinceResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link GetActivitySinceResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "getActivitySinceResponse")
    public JAXBElement<GetActivitySinceResponse> createGetActivitySinceResponse(GetActivitySinceResponse value) {
        return new JAXBElement<GetActivitySinceResponse>(_GetActivitySinceResponse_QNAME, GetActivitySinceResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetDepartments }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link GetDepartments }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "getDepartments")
    public JAXBElement<GetDepartments> createGetDepartments(GetDepartments value) {
        return new JAXBElement<GetDepartments>(_GetDepartments_QNAME, GetDepartments.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetDepartmentsResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link GetDepartmentsResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "getDepartmentsResponse")
    public JAXBElement<GetDepartmentsResponse> createGetDepartmentsResponse(GetDepartmentsResponse value) {
        return new JAXBElement<GetDepartmentsResponse>(_GetDepartmentsResponse_QNAME, GetDepartmentsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetEmployee }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link GetEmployee }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "getEmployee")
    public JAXBElement<GetEmployee> createGetEmployee(GetEmployee value) {
        return new JAXBElement<GetEmployee>(_GetEmployee_QNAME, GetEmployee.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetEmployeeResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link GetEmployeeResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "getEmployeeResponse")
    public JAXBElement<GetEmployeeResponse> createGetEmployeeResponse(GetEmployeeResponse value) {
        return new JAXBElement<GetEmployeeResponse>(_GetEmployeeResponse_QNAME, GetEmployeeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetStatistics }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link GetStatistics }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "getStatistics")
    public JAXBElement<GetStatistics> createGetStatistics(GetStatistics value) {
        return new JAXBElement<GetStatistics>(_GetStatistics_QNAME, GetStatistics.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetStatisticsResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link GetStatisticsResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "getStatisticsResponse")
    public JAXBElement<GetStatisticsResponse> createGetStatisticsResponse(GetStatisticsResponse value) {
        return new JAXBElement<GetStatisticsResponse>(_GetStatisticsResponse_QNAME, GetStatisticsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ListEmployees }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ListEmployees }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "listEmployees")
    public JAXBElement<ListEmployees> createListEmployees(ListEmployees value) {
        return new JAXBElement<ListEmployees>(_ListEmployees_QNAME, ListEmployees.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ListEmployeesResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ListEmployeesResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "listEmployeesResponse")
    public JAXBElement<ListEmployeesResponse> createListEmployeesResponse(ListEmployeesResponse value) {
        return new JAXBElement<ListEmployeesResponse>(_ListEmployeesResponse_QNAME, ListEmployeesResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchEmployees }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link SearchEmployees }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "searchEmployees")
    public JAXBElement<SearchEmployees> createSearchEmployees(SearchEmployees value) {
        return new JAXBElement<SearchEmployees>(_SearchEmployees_QNAME, SearchEmployees.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchEmployeesResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link SearchEmployeesResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "searchEmployeesResponse")
    public JAXBElement<SearchEmployeesResponse> createSearchEmployeesResponse(SearchEmployeesResponse value) {
        return new JAXBElement<SearchEmployeesResponse>(_SearchEmployeesResponse_QNAME, SearchEmployeesResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateEmployee }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link UpdateEmployee }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "updateEmployee")
    public JAXBElement<UpdateEmployee> createUpdateEmployee(UpdateEmployee value) {
        return new JAXBElement<UpdateEmployee>(_UpdateEmployee_QNAME, UpdateEmployee.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateEmployeeResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link UpdateEmployeeResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.employee/", name = "updateEmployeeResponse")
    public JAXBElement<UpdateEmployeeResponse> createUpdateEmployeeResponse(UpdateEmployeeResponse value) {
        return new JAXBElement<UpdateEmployeeResponse>(_UpdateEmployeeResponse_QNAME, UpdateEmployeeResponse.class, null, value);
    }

}
