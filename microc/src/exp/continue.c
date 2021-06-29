int main()
{
    int i ;
    int n = 0;
    for(i=0;i<5;i++)
    {
        if(i<2)
            continue;
        n=n+i;
    }
}