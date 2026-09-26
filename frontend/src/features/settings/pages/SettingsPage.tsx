import { ContractCard } from "../../contracts/components/ContractCard";
import { TelegramSettingsCard } from "../components/TelegramSettingsCard";
import { ReminderSettingsCard } from "../components/ReminderSettingsCard";
import { ReminderHistory } from "../../reminders/components/ReminderHistory";
import { useWorkspace } from "../../../app/WorkspaceContext";

export function SettingsPage() {
  const { contract } = useWorkspace();

  return (
    <div className="space-y-4">
      {contract && <ContractCard contract={contract} />}
      <TelegramSettingsCard />
      <ReminderSettingsCard />
      <ReminderHistory />
    </div>
  );
}
