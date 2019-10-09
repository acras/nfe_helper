package utils;

public class EncryptString
{
  public static void main(String[] args)
  {
    if (args.length == 0)
    {
      System.err.println("Usage: java EncryptString <string to encrypt>");
      System.exit(1);
    }

    try
    {
      String str = GenericEncryption.encryptString(args[0]);

      System.out.print(str);
      System.out.print("\n");
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }
}
