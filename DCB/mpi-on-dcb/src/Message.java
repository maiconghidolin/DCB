import java.io.*;
import java.util.Comparator;

///////////////////////////////// INICIO CLASSE MESSAGE /////////////////////////////////////		
		
	public class Message implements Serializable, Comparator
	{
		public String FederationSource;
		public String FederateSource;
		public String FederationDestination;
		public String FederateDestination;
		public String AttributeID;
		public String Value;
		public String LVT;
		public String OwnerFederate;
		public String NextOwnerFederate;
        public String Operation;

        public Message(){
        }

		public Message (String Operation, String FederationSource,String FederateSource,String FederationDestination,String FederateDestination,String AttributeID, String Value,String LVT,String OwnerFederate,String NextOwnerFederate)
		{
			this.FederationSource = FederationSource;
			this.FederateSource = FederateSource;
			this.FederationDestination = FederationDestination;
			this.FederateDestination = FederateDestination;
			this.AttributeID = AttributeID;
			this.Value = Value;
			this.LVT = LVT;
			this.OwnerFederate = OwnerFederate;
			this.NextOwnerFederate = NextOwnerFederate;
            this.Operation = Operation;
		}

        public String toString(){
            return "\nOperation: "+ this.Operation +
            "\nFederationSource: "+ this.FederationSource +
            "\nFederateSource: "+this.FederateSource +
            "\nFederationDestination: "+ this.FederationDestination +
            "\nFederateDestination: "+this.FederateDestination +
            "\nAttributeID: "+this.AttributeID +
            "\nValue: "+this.Value +
            "\nLVT: "+this.LVT +
            "\nOwnerFederate: "+this.OwnerFederate +
            "\nNextOwnerFederate: "+this.NextOwnerFederate;
        }

    public int compare(Object arg0, Object arg1) {
        return ((Message)arg0).LVT.compareTo(((Message)arg1).LVT);
    }
	}
		
//////////////////////////////// FIM CLASSE MESSAGE /////////////////////////////////////////
	