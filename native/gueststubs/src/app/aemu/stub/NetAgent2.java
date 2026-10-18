package app.aemu.stub;

import android.content.Context;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkAgentConfig;
import android.net.NetworkCapabilities;
import android.os.Looper;

/**
 * The guest's network for Android 12+, where the agent NetStub is built on lost its constructor with a NetworkInfo:
 * the agent is made from a NetworkAgentConfig, then registered and marked connected.
 */
public class NetAgent2 extends NetworkAgent {
    NetAgent2(Context c, Looper l, NetworkCapabilities nc, LinkProperties lp, NetworkAgentConfig cfg) {
        super(c, l, "AEmuNet", nc, lp, 60, cfg, null);
    }

    @Override
    protected void unwanted() { }

    static void start(Context ctx, Looper l, NetworkCapabilities nc, LinkProperties lp) throws Exception {
        // (required of a default network since 9 and 12, and not there to add before)
        for (int cap : new int[]{18 /* NOT_ROAMING */, 20 /* NOT_CONGESTED */, 21 /* NOT_SUSPENDED */, 28 /* NOT_VCN_MANAGED */}) {
            try { NetStub.call(nc, "addCapability", new Class<?>[]{int.class}, cap); } catch (Throwable t) { /* an older release */ }
        }
        Object b = NetStub.newObj("android.net.NetworkAgentConfig$Builder", new Class<?>[0]);
        NetStub.call(b, "setLegacyType", new Class<?>[]{int.class}, 9 /* TYPE_ETHERNET */);
        NetStub.call(b, "setLegacyTypeName", new Class<?>[]{String.class}, "ETHERNET");
        NetworkAgentConfig cfg = (NetworkAgentConfig) b.getClass().getMethod("build").invoke(b);
        NetAgent2 a = new NetAgent2(ctx, l, nc, lp, cfg);
        a.register();
        a.markConnected();
    }
}
