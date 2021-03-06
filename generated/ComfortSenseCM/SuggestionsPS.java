//
// Copyright (c) ZeroC, Inc. All rights reserved.
//
//
// Ice version 3.7.5
//
// <auto-generated>
//
// Generated from file `ComfortSense.ice'
//
// Warning: do not edit this file.
//
// </auto-generated>
//

package ComfortSenseCM;

public interface SuggestionsPS extends com.zeroc.Ice.Object
{
    void suggestedContent(String username, String warningType, int currentValue, int limitValue, boolean weatherAlarm, String currentLocation, String userPref, String[] suggestedLocations, com.zeroc.Ice.Current current);

    /** @hidden */
    static final String[] _iceIds =
    {
        "::ComfortSenseCM::SuggestionsPS",
        "::Ice::Object"
    };

    @Override
    default String[] ice_ids(com.zeroc.Ice.Current current)
    {
        return _iceIds;
    }

    @Override
    default String ice_id(com.zeroc.Ice.Current current)
    {
        return ice_staticId();
    }

    static String ice_staticId()
    {
        return "::ComfortSenseCM::SuggestionsPS";
    }

    /**
     * @hidden
     * @param obj -
     * @param inS -
     * @param current -
     * @return -
    **/
    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_suggestedContent(SuggestionsPS obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_username;
        String iceP_warningType;
        int iceP_currentValue;
        int iceP_limitValue;
        boolean iceP_weatherAlarm;
        String iceP_currentLocation;
        String iceP_userPref;
        String[] iceP_suggestedLocations;
        iceP_username = istr.readString();
        iceP_warningType = istr.readString();
        iceP_currentValue = istr.readInt();
        iceP_limitValue = istr.readInt();
        iceP_weatherAlarm = istr.readBool();
        iceP_currentLocation = istr.readString();
        iceP_userPref = istr.readString();
        iceP_suggestedLocations = istr.readStringSeq();
        inS.endReadParams();
        obj.suggestedContent(iceP_username, iceP_warningType, iceP_currentValue, iceP_limitValue, iceP_weatherAlarm, iceP_currentLocation, iceP_userPref, iceP_suggestedLocations, current);
        return inS.setResult(inS.writeEmptyParams());
    }

    /** @hidden */
    final static String[] _iceOps =
    {
        "ice_id",
        "ice_ids",
        "ice_isA",
        "ice_ping",
        "suggestedContent"
    };

    /** @hidden */
    @Override
    default java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceDispatch(com.zeroc.IceInternal.Incoming in, com.zeroc.Ice.Current current)
        throws com.zeroc.Ice.UserException
    {
        int pos = java.util.Arrays.binarySearch(_iceOps, current.operation);
        if(pos < 0)
        {
            throw new com.zeroc.Ice.OperationNotExistException(current.id, current.facet, current.operation);
        }

        switch(pos)
        {
            case 0:
            {
                return com.zeroc.Ice.Object._iceD_ice_id(this, in, current);
            }
            case 1:
            {
                return com.zeroc.Ice.Object._iceD_ice_ids(this, in, current);
            }
            case 2:
            {
                return com.zeroc.Ice.Object._iceD_ice_isA(this, in, current);
            }
            case 3:
            {
                return com.zeroc.Ice.Object._iceD_ice_ping(this, in, current);
            }
            case 4:
            {
                return _iceD_suggestedContent(this, in, current);
            }
        }

        assert(false);
        throw new com.zeroc.Ice.OperationNotExistException(current.id, current.facet, current.operation);
    }
}
