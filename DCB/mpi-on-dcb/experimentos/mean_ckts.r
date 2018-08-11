
probabilistic <- read.csv(file="probabilistic/susu.csv", header=TRUE, sep=",")
quaglia <- read.csv(file="quaglia/susu.csv", header=TRUE, sep=",")
pe <- read.csv(file="pe/susu.csv", header=TRUE, sep=",")
periodic <- read.csv(file="nom-coordinate/susu.csv", header=TRUE, sep=",")

cktprob <- mean(probabilistic$totalCheckpoints, na.rm=TRUE)
cktqua <- mean(quaglia$totalCheckpoints, na.rm=TRUE)
cktpe <- mean(pe$totalCheckpoints, na.rm=TRUE)
cktperiodic <- mean(periodic$totalCheckpoints, na.rm=TRUE)


jpeg(filename="/home/aluno-uffs/bosta/periodic-and-probabilistic-checkpoints/checkpoints.jpeg")

barplot(c(cktperiodic, cktprob, cktqua, cktpe), xpd = F, density=c(20,20,20,20), col="brown", border="brown", beside=T, ylab="Checkpoints created", ylim=c(700, 1000), font.lab=2, names.arg=c("Periodic","Probabilistic","Quaglia", "Probabilistic(PE)"))


dev.off()
