package org.unidal.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

public class Networks {
   public static IpHelper forIp() {
      return IpHelper.INSTANCE;
   }

   public static void main(String[] args) {
      System.out.println("IP address selected: " + new IpHelper(true).getLocalHostAddress());
   }

   public static class IpHelper {
      private static IpHelper INSTANCE = new IpHelper(false);

      private InetAddress m_local;

      private boolean m_verbose;

      private IpHelper(boolean verbose) {
         m_verbose = verbose;
         initialize();
      }

      private Object buildAddressFlags(InetAddress ia) {
         StringBuilder sb = new StringBuilder(64);

         try {
            if (ia.isAnyLocalAddress()) {
               sb.append(",ANY");
            }

            if (ia.isLinkLocalAddress()) {
               sb.append(",LINK");
            }

            if (ia.isLoopbackAddress()) {
               sb.append(",LOOPBACK");
            }

            if (ia.isSiteLocalAddress()) {
               sb.append(",SITE");
            }

            if (ia.isMulticastAddress()) {
               sb.append(",MULTICAST");
            }
         } catch (Exception e) {
            // ignore it
         }

         if (sb.length() > 0) {
            return sb.substring(1);
         } else {
            return "";
         }
      }

      private String buildInterfaceFlags(NetworkInterface ni) {
         StringBuilder sb = new StringBuilder(64);

         try {
            if (ni.isUp()) {
               sb.append(",UP");
            }

            if (ni.isLoopback()) {
               sb.append(",LOOPBACK");
            }

            if (ni.isPointToPoint()) {
               sb.append(",P2P");
            }

            if (ni.isVirtual()) {
               sb.append(",VIRTUAL");
            }

            if (ni.supportsMulticast()) {
               sb.append(",MULTICAST");
            }
         } catch (Exception e) {
            // ignore it
         }

         if (sb.length() > 0) {
            return sb.substring(1);
         } else {
            return "";
         }
      }

      private InetAddress getConfiguredAddress() {
         String ip = System.getProperty("host.ip");

         print("Checking IP address from property(host.ip) ... ");

         if (ip != null) {
            println("Found " + ip);
         } else {
            println(null);
         }

         if (ip == null) {
            print("Checking IP address from env(HOST_IP) ... ");

            ip = System.getenv("HOST_IP");

            if (ip != null) {
               println("Found " + ip);
            } else {
               println(null);
            }
         }

         if (ip != null) {
            try {
               return InetAddress.getByName(ip);
            } catch (Exception e) {
               // invalid ip address configured, try to auto detect below
               println("[WARN] Unable to resolve IP address(%s)! %s, IGNORED.", ip, e);
            }
         }

         return null;
      }

      private InetAddress getDetectedAddress() {
         try {
            List<NetworkInterface> nis = Collections.list(NetworkInterface.getNetworkInterfaces());
            InetAddress found = null;

            for (NetworkInterface ni : nis) {
               println("%s: flags=<%s> mtu %s", ni.getDisplayName(), buildInterfaceFlags(ni), ni.getMTU());

               if (ni.isUp()) {
                  try {
                     List<InetAddress> ias = Collections.list(ni.getInetAddresses());

                     for (InetAddress ia : ias) {
                        boolean inet4 = ia instanceof Inet4Address;

                        println("     %s %s flags=<%s>", inet4 ? "inet" : "inet6", ia.getHostAddress(),
                              buildAddressFlags(ia));

                        if (inet4 && !ia.isLinkLocalAddress()) {
                           if (ia.isLoopbackAddress() || ia.isSiteLocalAddress()) {
                              if (found == null) {
                                 found = ia;
                              } else if (found.isLoopbackAddress() && ia.isSiteLocalAddress()) {
                                 // site local address has higher priority than
                                 // loopback address
                                 found = ia;
                              } else if (found.isSiteLocalAddress() && ia.isSiteLocalAddress()) {
                                 // site local address with a host name has higher
                                 // priority than one without host name
                                 if (found.getHostName().equals(found.getHostAddress())
                                       && !ia.getHostName().equals(ia.getHostAddress())) {
                                    found = ia;
                                 }
                              }
                           }
                        }
                     }
                  } catch (Exception e) {
                     // ignore
                  }
               }
            }

            return found;
         } catch (SocketException e) {
            // ignore it
            println("[ERROR] %s", e);
            return null;
         }
      }

      public byte[] getLocalAddress() {
         return m_local.getAddress();
      }

      public String getLocalHostAddress() {
         return m_local.getHostAddress();
      }

      public String getLocalHostName() {
         try {
            return InetAddress.getLocalHost().getHostName();
         } catch (UnknownHostException e) {
            return m_local.getHostName();
         }
      }

      private void initialize() {
         InetAddress address = getConfiguredAddress();

         if (address == null) {
            address = getDetectedAddress();
         }

         if (address != null) {
            m_local = address;
         } else {
            throw new IllegalStateException("No IP address was detected!");
         }
      }

      private void print(String pattern, Object... args) {
         if (m_verbose) {
            System.out.print(String.format(pattern, args));
         }
      }

      private void println(String pattern, Object... args) {
         if (m_verbose) {
            if (pattern != null) {
               System.out.println(String.format(pattern, args));
            } else {
               System.out.println();
            }
         }
      }
   }
}