package utils;

public class DecryptString
{
  public static void main(String[] args)
  {
    if (args.length == 0)
    {
      System.err.println("Usage: java DecryptString <string to decrypt>");
      System.exit(1);
    }

    try
    {
      String str = GenericEncryption.decryptString(args[0]);

      System.out.print(str);
      System.out.print(" (");
      System.out.print(str.length());
      System.out.print(" characters)");
      System.out.print("\n");
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }
}
