
public class Destination // classe que armazena informa��es sobre os atributos destino (idfederacao, idfederado, atributodestino)
{

    public String federationid;
    public String federateid;
    public String attributeID;

    public Destination(String federationid, String federateid, String attributeID) {
        this.federationid = federationid;
        this.federateid = federateid;
        this.attributeID = attributeID;
    }
}