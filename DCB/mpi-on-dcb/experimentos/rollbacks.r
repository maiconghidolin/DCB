#Create data


#set.seed(112)
#data=matrix(sample(1:30,15) , nrow=3)

data = matrix(
    c(84880, 86355, 93770),
    nrow=1, 
    ncol=3, 
    byrow=TRUE
)



png(filename="/home/ricardo/Documents/rollbacktime.png",  width=600, height =600, res=100)

colnames(data)=c("I", "II", "III")
rownames(data)=c("Rollback time")
 
# Get the stacked barplot
barplot(data, density=c(20,20,20) , angle=c(0,45,90) , col="brown", border="brown", space=0.02, font.axis=2, xlab="Execution")

# Grouped barplot
barplot(data, density=c(20,20,20) , col="brown", border="brown", font.axis=2, beside=T, legend=rownames(data), xlab="Execution", ylim=c(0, 200000), font.lab=2)

dev.off()

